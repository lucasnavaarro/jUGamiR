package com.jugamir.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

/** Service-layer boundaries; HTTP multipart limits are tested separately by scripts/qa/files.mjs. */
class ImageAcceptanceBoundaryTest {
    @TempDir Path root;
    byte[] png(int width, int height) throws IOException {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),"png",out);
        return out.toByteArray();
    }
    byte[] padded(int size) throws IOException {
        byte[] base=png(2,2), payload=new byte[size-base.length-12];
        // An unknown ancillary chunk is legal PNG data and is removed by raster reencoding.
        byte[] type="qaQa".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        CRC32 crc=new CRC32();crc.update(type);crc.update(payload);
        var out=new ByteArrayOutputStream(size);
        out.write(base,0,base.length-12);out.write(ByteBuffer.allocate(4).putInt(payload.length).array());
        out.write(type);out.write(payload);out.write(ByteBuffer.allocate(4).putInt((int)crc.getValue()).array());
        out.write(base,base.length-12,12);
        assertEquals(size,out.size());return out.toByteArray();
    }
    @Test void IMG04_byteLimitBelowAtAndAboveTenMiB() throws Exception {
        var storage=new ImageStorage(root.toString());
        for(int delta:new int[]{-1,0,1}) {
            byte[] data=padded((int)ImageStorage.MAX_IMAGE_BYTES+delta);
            var upload=new MockMultipartFile("file","qa.png","image/png",data);
            if(delta<=0){storage.upload(upload,"imagenes/bytes"+delta+".png");assertTrue(Files.size(root.resolve("imagenes/bytes"+delta+".png"))<1000);}
            else assertThrows(IllegalArgumentException.class,()->storage.upload(upload,"imagenes/too-large.png"));
        }
    }
    @Test void IMG04_pixelLimitBelowAtAndAboveSixteenMillion() throws Exception {
        for(int width:new int[]{3999,4000,4001}) {
            Path file=root.resolve("pixels"+width+".png");Files.write(file,png(width,4000));
            if(width<=4000)assertDoesNotThrow(()->RasterImages.sanitize(file,"pixels.png"));
            else assertThrows(IllegalArgumentException.class,()->RasterImages.sanitize(file,"pixels.png"));
        }
    }
    @Test void IMG08_entryLimitExactlyFiveHundredAndFiveHundredOne() throws Exception {
        for(int count:new int[]{500,501}) {
            var data=new ByteArrayOutputStream();
            try(var zip=new ZipOutputStream(data)){
                for(int i=0;i<count-1;i++){zip.putNextEntry(new ZipEntry("imagenes/d"+i+"/"));zip.closeEntry();}
                zip.putNextEntry(new ZipEntry("imagenes/valid.png"));zip.write(png(2,2));zip.closeEntry();
            }
            var storage=new ImageStorage(root.resolve("entries"+count).toString());
            var upload=new MockMultipartFile("file",data.toByteArray());
            if(count==500)assertDoesNotThrow(()->storage.uploadZip(upload));
            else assertThrows(IllegalArgumentException.class,()->storage.uploadZip(upload));
        }
    }
    @Test void IMG08_expandedBudgetBelowAtAndAboveTwoHundredMiB() throws Exception {
        byte[] block=padded((int)ImageStorage.MAX_IMAGE_BYTES);
        for(int delta:new int[]{-1,0,1}){
            Path archive=root.resolve("budget"+delta+".zip");
            try(var zip=new ZipOutputStream(Files.newOutputStream(archive))){
                for(int i=0;i<20;i++){
                    byte[] bytes=i==19&&delta<0?padded(block.length-1):block;
                    ZipEntry entry=new ZipEntry("imagenes/"+i+".png");entry.setMethod(ZipEntry.STORED);entry.setSize(bytes.length);
                    CRC32 crc=new CRC32();crc.update(bytes);entry.setCrc(crc.getValue());zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();
                }
                if(delta>0){zip.putNextEntry(new ZipEntry("imagenes/extra.png"));zip.write(png(2,2));zip.closeEntry();}
            }
            // Stream the large archive from disk instead of holding a 200 MiB multipart array.
            var upload=new MockMultipartFile("file",new byte[0]){
                @Override public long getSize(){return archive.toFile().length();}
                @Override public InputStream getInputStream() throws IOException{return Files.newInputStream(archive);}
                @Override public void transferTo(File destination) throws IOException{Files.copy(archive,destination.toPath(),StandardCopyOption.REPLACE_EXISTING);}
            };
            var storage=new ImageStorage(root.resolve("budget-out"+delta).toString());
            if(delta<=0)assertDoesNotThrow(()->storage.uploadZip(upload));
            else assertThrows(IllegalArgumentException.class,()->storage.uploadZip(upload));
            Files.delete(archive);
        }
    }
    @Test void IMG09_thirtySecondProcessingBudgetIsCheckedBeforePublishing() throws Exception {
        var archive=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(archive)){zip.putNextEntry(new ZipEntry("imagenes/slow.png"));zip.write(png(2,2));zip.closeEntry();}
        var upload=new MockMultipartFile("file",new byte[0]){
            @Override public long getSize(){return archive.size();}
            @Override public void transferTo(File destination) throws IOException {
                try(var input=getInputStream()){Files.copy(input,destination.toPath(),StandardCopyOption.REPLACE_EXISTING);}
            }
            @Override public InputStream getInputStream(){return new FilterInputStream(new ByteArrayInputStream(archive.toByteArray())){
            boolean delayed=false;
            @Override public int read(byte[] b,int off,int len) throws IOException {if(!delayed){delayed=true;try{Thread.sleep(31_000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}}return super.read(b,off,len);}
        };}};
        assertThrows(IllegalArgumentException.class,()->new ImageStorage(root.toString()).uploadZip(upload));
        assertFalse(Files.exists(root.resolve("imagenes/slow.png")));
    }
}
