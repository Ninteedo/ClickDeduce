import {Subtree} from "./subtree";

class SubtreeToolbox {
    private readonly container: HTMLDivElement;

    private readonly entries: ToolboxEntry[];
    private newEntryCount: number = 0;

    constructor(container: HTMLDivElement) {
        this.container = container;
        this.entries = [];
    }

    private updateContents(): void {
        const newChildren: HTMLDivElement[] = this.entries.map(entry => entry.getElement());
        this.container.replaceChildren(...newChildren);
        this.entries.forEach(entry => entry.update());
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
    console.log('Adding subtree to toolbox');
    getSubtreeToolbox().addSubtree(subtree.copy());
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
        const containerWidth = getSubtreeToolbox().getElement().clientWidth;
        const myWidth = this.element.scrollWidth;
        console.log(`Container width: ${containerWidth}, my width: ${myWidth}`);
        if (myWidth > containerWidth) {
            console.log('Scaling down');
            this.element.style.transform = `scale(${containerWidth / myWidth})`;
        }
    }
}
