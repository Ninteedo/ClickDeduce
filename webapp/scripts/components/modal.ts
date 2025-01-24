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
        this.element.classList.add('visible');
        this.blocker.classList.add('visible');
    }

    hide(): void {
        this.element.classList.remove('visible');
        this.blocker.classList.remove('visible');
    }
}
