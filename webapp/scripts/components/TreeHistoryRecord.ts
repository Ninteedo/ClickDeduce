export default interface TreeHistoryRecord {
    mode: string;
    html: string;
    nodeString: string;
    lang: string;
}

export function treeHistoryRecordsEqual(a: TreeHistoryRecord, b: TreeHistoryRecord): boolean {
    return a.mode === b.mode && a.html === b.html && a.nodeString === b.nodeString && a.lang === b.lang;
}
