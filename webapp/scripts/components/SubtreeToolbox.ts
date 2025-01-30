import {Subtree} from "./subtree";

class SubtreeToolbox {
    private readonly container: HTMLDivElement;

    private readonly entries: ToolboxEntry[];
    private newEntryCount: number = 0;

    // private readonly DRAG_HIGHLIGHT_CLASS = 'drag-highlight';

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
        this.entries.push(new ToolboxEntry(subtree, `Subtree ${++this.newEntryCount}`));
        this.updateContents();
    }

    getElement(): HTMLDivElement {
        return this.container;
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

class ToolboxEntry {
    private subtree: Subtree;
    private name: string;

    private readonly element: HTMLDivElement;

    constructor(subtree: Subtree, name: string) {
        this.subtree = subtree;
        this.name = name;

        this.element = document.createElement('div');
        this.element.classList.add('toolbox-entry');
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

    getName(): string {
        return this.name;
    }

    getElement(): HTMLDivElement {
        return this.element;
    }

    update(): void {
        this.autoScale();
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
