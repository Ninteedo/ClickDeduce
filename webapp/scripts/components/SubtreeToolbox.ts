import {Subtree} from "./subtree";
import {pauseFileDragAndDrop, resumeFileDragAndDrop} from "../saveLoad";

class SubtreeToolbox {
    private readonly container: HTMLDivElement;

    private readonly entries: ToolboxEntry[];
    private newEntryCount: number = 0;

    private readonly HIDDEN_CLASS = 'hidden';

    constructor(container: HTMLDivElement) {
        this.container = container;
        this.entries = [];
        this.updateContents();
    }

    private updateContents(): void {
        if (this.entries.length > 0) {
            this.container.classList.remove(this.HIDDEN_CLASS);
            const newChildren: HTMLDivElement[] = this.entries.map(entry => entry.getElement());
            this.container.replaceChildren(...newChildren);
            this.entries.forEach(entry => entry.update());
        } else {
            this.container.classList.add(this.HIDDEN_CLASS);
        }
    }

    addSubtree(subtree: Subtree): void {
        this.entries.push(new ToolboxEntry(subtree, this.newEntryCount++));
        this.updateContents();
    }

    removeEntry(entry: ToolboxEntry): void {
        const index = this.entries.indexOf(entry);
        if (index === -1) throw new Error('Entry not found');
        this.entries.splice(index, 1);
        this.updateContents();
        entry.getElement().remove();
    }

    moveEntry(entry: ToolboxEntry, change: number): void {
        const index = this.entries.indexOf(entry);
        if (index === -1) throw new Error('Entry not found');
        let newIndex = index + change;
        if (newIndex < 0) newIndex += this.entries.length;
        if (newIndex >= this.entries.length) newIndex -= this.entries.length;
        this.entries.splice(index, 1);
        this.entries.splice(newIndex, 0, entry);
        this.updateContents();
    }

    getElement(): HTMLDivElement {
        return this.container;
    }

    getEntryWithID(id: number): ToolboxEntry | undefined {
        return this.entries.find(entry => entry.getID() === id);
    }

    // private setUpDropZone(): void {
    //     this.container.addEventListener('dragenter', event => {
    //         this.container.classList.add(this.DRAG_HIGHLIGHT_CLASS);
    //     });
    //     this.container.addEventListener('dragleave', event => {
    //         this.container.classList.remove(this.DRAG_HIGHLIGHT_CLASS);
    //     });
    //     this.container.addEventListener('dragover', event => {
    //         event.preventDefault();
    //     });
    //     this.container.addEventListener('drop', event => {
    //
    //     });
    // }
}

let subtreeToolbox: SubtreeToolbox | null;

export function getSubtreeToolbox(): SubtreeToolbox {
    if (!subtreeToolbox) {
        const container = document.getElementById('subtree-toolbox');
        if (!(container instanceof HTMLDivElement)) throw new Error('Subtree toolbox container not found');
        subtreeToolbox = new SubtreeToolbox(container);
    }
    return subtreeToolbox;
}

export function addSubtreeToToolbox(subtree: Subtree): void {
    getSubtreeToolbox().addSubtree(subtree.copy(false, "edit"));
}

export class ToolboxEntry {
    private readonly subtree: Subtree;
    private readonly id: number

    private readonly element: HTMLDivElement;

    constructor(subtree: Subtree, id: number) {
        this.subtree = subtree;
        this.id = id;

        this.element = document.createElement('div');
        this.element.classList.add('toolbox-entry');
        this.element.setAttribute('data-id', id.toString());
        this.element.appendChild(subtree.getElement());

        this.setUpDrag();
        this.subtree.disableAllInputs();

        this.autoScale();
    }

    private setUpDrag(): void {
        this.element.draggable = true;
        this.element.addEventListener('dragstart', event => {
            event.dataTransfer?.setData('plain/subtreeNodeString', this.subtree.getNodeString());
            pauseFileDragAndDrop();
        });
        this.element.addEventListener('dragend', () => {
            resumeFileDragAndDrop();
        });
    }

    getSubtree(): Subtree {
        return this.subtree;
    }

    getElement(): HTMLDivElement {
        return this.element;
    }

    update(): void {
        this.autoScale();
    }

    getID(): number {
        return this.id;
    }

    private autoScale(): void {
        this.element.style.zoom = '1';
        const containerWidth = getSubtreeToolbox().getElement().clientWidth;
        const myWidth = this.element.clientWidth;
        if (myWidth > containerWidth) {
            this.element.style.zoom = `${containerWidth / myWidth}`;
        }
    }
}
