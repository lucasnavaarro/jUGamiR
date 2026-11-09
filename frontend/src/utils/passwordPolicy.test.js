import assert from 'node:assert/strict';
import { test } from 'node:test';
import { isValidPassword } from './passwordPolicy.js';

test('accepts a dot and all other printable ASCII punctuation', () => {
    for (let code = 33; code <= 126; code++) {
        const symbol = String.fromCharCode(code);
        if (!/[A-Za-z0-9]/.test(symbol)) {
            assert.equal(isValidPassword(`Aa12345${symbol}`), true, `symbol ${symbol}`);
        }
    }
});

test('rejects missing requirements, whitespace and non-ASCII characters', () => {
    for (const password of [null, '', 'Aa1234.', 'aa12345.', 'AA12345.', 'Abcdefg.', 'Aa123456', 'Aa12345 ', 'Aa12345. ', 'Aa12345.\n', 'Aa12345.\r', 'Aa12345.\t', 'Aa12345.\u00a0', 'Aa12345.ñ']) {
        assert.equal(isValidPassword(password), false);
    }
});
