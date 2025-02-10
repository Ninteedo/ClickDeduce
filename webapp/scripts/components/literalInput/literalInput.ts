import {findTreePathFromElement} from "../../interface";
import {handleLiteralChanged} from "../../actions";
import {AbstractTreeInput} from "../abstractTreeInput";
import {getTree, getTreePathOfElement} from "../../globals/elements";
// @ts-ignore
import TrueSvg from '../../../images/true.svg';
// @ts-ignore
import FalseSvg from '../../../images/false.svg';
import {LiteralPlaceholder} from "./literalPlaceholder";
import {handleTabPressed, setNextFocusElement, setNextFocusTreePath} from "../../focus";
import {ClassDict} from "../../globals/classDict";

export class LiteralInput implements AbstractTreeInput {
    protected readonly input: HTMLInputElement;
    protected readonly linkedPlaceholders: LiteralPlaceholder[];
    public readonly treePath: string;
    protected readonly initialValue: string;

    protected readonly MIN_WIDTH: number = 2;

    constructor(input: HTMLInputElement) {
        this.input = input;
        this.treePath = getTreePathOfElement(input);
        this.linkedPlaceholders = this.createLinkedPlaceholders();
        this.initialValue = this.getValue();

        this.setupEventListeners();
        this.onInit();
    }

    focus(): void {
        this.input.focus();
        this.input.select();
        this.onFocused();
    }

    blur(): void {
        this.input.blur();
        this.onBlurred();
    }

    disable() {
        this.input.disabled = true;
        this.input.readOnly = true;
    }

    enable() {
        this.input.disabled = false;
        this.input.readOnly = false
    }

    getTreePath(): string {
        return this.treePath;
    }

    private createLinkedPlaceholders(): LiteralPlaceholder[] {
        return Array.from(getTree().querySelectorAll(`.${ClassDict.LITERAL}`))
            .filter((elem: Element) => elem.getAttribute('data-origin') === this.treePath)
            .map((elem: Element) => new LiteralPlaceholder(elem as HTMLDivElement, this));
    }

    private setupEventListeners(): void {
        this.input.addEventListener('input', () => this.onInput());
        this.input.addEventListener('focus', () => this.onFocused());
        this.input.addEventListener('blur', event => this.onBlurred(event));
        this.input.addEventListener('keydown', event => this.handleKeydown(event));
    }

    protected onInit(): void {

    }

    protected onInput(): void {
        this.updateInputWidth();
        this.updateLinkedInputPlaceholders();
    }

    protected handleKeydown(evt: KeyboardEvent): void {
        if (evt.key === 'Tab') {
            handleTabPressed(evt);
        } else if (evt.key === 'Enter') {
            evt.preventDefault();
            this.submit();
        }
    }

    public submit(): void {
        setNextFocusElement(this);
        this.handleInputChanged();
    }

    protected onFocused(): void {

    }

    protected onBlurred(event: FocusEvent | undefined = undefined): void {
        if (event && event.relatedTarget instanceof HTMLElement) {
            setNextFocusTreePath(findTreePathFromElement(event.relatedTarget));
        }
        this.handleInputChanged(true);
    }

    protected handleInputChanged(doNotFocus: boolean = false): void {
        if (this.getValue() === this.initialValue) return;
        handleLiteralChanged(this.input, doNotFocus);
    }

    private updateInputWidth(): void {
        this.input.style.width = Math.max(this.MIN_WIDTH, this.getValue().length) + "ch";
    }

    private updateLinkedInputPlaceholders(): void {
        this.linkedPlaceholders.forEach((placeholder: LiteralPlaceholder) => {
            placeholder.setContent(this.getPlaceholderContent());
        });
    }

    public setValue(value: string): void {
        if (this.getValue() === value) return;
        this.input.value = value;
        this.onInput();
    }

    public getValue(): string {
        return this.input.value;
    }

    public addGuideHighlight(): void {
        this.input.classList.add(ClassDict.GUIDE_HIGHLIGHT);
    }

    public removeGuideHighlight(): void {
        this.input.classList.remove(ClassDict.GUIDE_HIGHLIGHT);
    }

    protected getPlaceholderContent(): HTMLElement {
        const div = document.createElement('div');
        div.innerText = this.getValue();
        return div;
    }
}
