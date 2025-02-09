// @ts-ignore
import PhantomSvg from '../../images/phantom.svg';
import {ClassDict} from "../globals/classDict";

export class PhantomIndicator {
    public readonly element: HTMLDivElement;

    constructor() {
        this.element = document.createElement('div');
        this.element.classList.add(ClassDict.PHANTOM_INDICATOR);
        this.element.style.content = `url("${PhantomSvg}")`;
    }

    public show(): void {
        this.element.classList.add(ClassDict.VISIBLE);
    }

    public hide(): void {
        this.element.classList.remove(ClassDict.VISIBLE);
    }
}
