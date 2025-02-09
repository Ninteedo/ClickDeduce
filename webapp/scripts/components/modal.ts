import {ClassDict} from "../globals/classDict";

export class Modal {
    private readonly element: HTMLDivElement;
    private readonly blocker: HTMLElement;

    constructor(element: HTMLDivElement, blocker: HTMLElement) {
        this.element = element;
        this.blocker = blocker;

        this.blocker.addEventListener('click', () => this.hide());
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.hide();
            }
        });
    }

    show(): void {
        this.element.classList.add(ClassDict.VISIBLE);
        this.blocker.classList.add(ClassDict.VISIBLE);
    }

    hide(): void {
        this.element.classList.remove(ClassDict.VISIBLE);
        this.blocker.classList.remove(ClassDict.VISIBLE);
    }
}
