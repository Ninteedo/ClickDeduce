import TreeHistoryRecord from "./TreeHistoryRecord";
import {updateTree} from "../treeManipulation";

export default class TreeHistoryManager {
    private records: Array<TreeHistoryRecord> = [];
    private recordIndex: number = 0;
    private undoButton: HTMLButtonElement;
    private redoButton: HTMLButtonElement;

    constructor(undoButton: HTMLButtonElement, redoButton: HTMLButtonElement) {
        this.undoButton = undoButton;
        this.redoButton = redoButton;
        this.updateButtons();
    }

    public addRecord(record: TreeHistoryRecord): void {
        if (this.records.length > 0 &&
            record.html === this.records[this.recordIndex].html &&
            record.nodeString === this.records[this.recordIndex].nodeString
        ) {
            return;
        }

        if (this.recordIndex < this.records.length - 1) {
            this.records = this.records.slice(0, this.recordIndex + 1);
        }
        this.recordIndex = this.records.push(record) - 1;
    }

    public useRecord(index: number): void {
        if (index >= 0 && index < this.records.length) {
            this.recordIndex = index;
            const entry = this.records[index];
            updateTree(entry.html, entry.nodeString, entry.mode, entry.lang, false);
        }
    }

    public reloadCurrentTree(): void {
        this.useRecord(this.recordIndex);
    }

    public undo(): void {
        if (this.recordIndex > 0) {
            this.useRecord(this.recordIndex - 1);
        }
    }

    public redo(): void {
        if (this.recordIndex < this.records.length - 1) {
            this.useRecord(this.recordIndex + 1);
        }
    }

    public updateButtons(): void {
        this.undoButton.disabled = this.recordIndex <= 0;
        this.redoButton.disabled = this.recordIndex >= this.records.length - 1;
    }
}
