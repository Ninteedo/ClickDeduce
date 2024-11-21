export interface AbstractTreeInput {
    focus(): void;
    blur(): void;
    disable(): void;
    enable(): void;

    getTreePath(): string;
}
