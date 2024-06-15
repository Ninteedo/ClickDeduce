import {expect, test} from "vitest";
import {compareTreePaths} from "../utils";

test('tree path comparison is correct', () => {
    expect(compareTreePaths('', '')).toBe(0);
    expect(compareTreePaths('0', '')).toBe(1);
    expect(compareTreePaths('', '0')).toBe(-1);
    expect(compareTreePaths('0', '0')).toBe(0);
    expect(compareTreePaths('0', '1')).toBe(-1);
    expect(compareTreePaths('1', '0')).toBe(1);
    expect(compareTreePaths('0', '0-0')).toBe(-1);
    expect(compareTreePaths('0-0', '0')).toBe(1);
    expect(compareTreePaths('0-0', '0-0')).toBe(0);
    expect(compareTreePaths('0-0', '0-1')).toBe(-1);
    expect(compareTreePaths('0-1', '0-0')).toBe(1);
    expect(compareTreePaths('0-0', '0-0-0')).toBe(-1);
})
