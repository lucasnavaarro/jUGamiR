// CI uses the locked Playwright package. Local runs may select an installed runtime.
const {chromium} = await import(process.env.QA_PLAYWRIGHT_MODULE || 'playwright');

export function launchBrowser() {
  return chromium.launch({
    headless: true,
    ...(process.env.QA_CHROMIUM_EXECUTABLE ? {executablePath: process.env.QA_CHROMIUM_EXECUTABLE} : {}),
  });
}
