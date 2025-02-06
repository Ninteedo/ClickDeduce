const VISIBLE_CLASS = 'visible';

export class PhantomIndicator {
    public readonly element: HTMLDivElement;

    constructor() {
        this.element = document.createElement('div');
        this.element.classList.add('phantom-indicator');
    }

    public show(): void {
        this.element.classList.add(VISIBLE_CLASS);
    }

    public hide(): void {
        this.element.classList.remove(VISIBLE_CLASS);
    }
}
