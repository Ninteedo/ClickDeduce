import {parseTreePath} from "../utils";
import {getContextMenuSelectedElement} from "./contextMenu";
import {createLiteralInput} from "./literalInput";
import {createExprSelector, replaceDisabledSelectInputs} from "./customExprSelector";
import {AbstractTreeInput} from "./abstractTreeInput";
import {RuleAnnotation} from "./ruleAnnotation";
import {getTreePathOfElement} from "../globals/elements";
import {getRootSubtree} from "../treeManipulation";
import {getNodeStringFromPath} from "../utility/parseNodeString";

export class Subtree {
    private readonly element: HTMLDivElement;
    private readonly parent: Subtree | null;
    private readonly children: Subtree[];

    private readonly nodeString: string;

    private readonly treePath: string;
    private readonly parsedTreePath: number[];

    private readonly nodeElement: HTMLDivElement;
    private readonly argsElement: HTMLDivElement | null;

    private readonly inputs: AbstractTreeInput[];
    private readonly allInputs: AbstractTreeInput[];

    private readonly ruleAnnotation: RuleAnnotation;

    private readonly isPhantom: boolean;

    private readonly HIGHLIGHT_CLASS = 'highlight';
    private readonly PHANTOM_CLASS = 'phantom';
    private readonly DATA_TREE_PATH = 'data-tree-path';

    constructor(element: HTMLDivElement, parent: Subtree | null, nodeString: string) {
        this.element = element;
        this.parent = parent;

        this.nodeString = nodeString;

        this.treePath = this.element.getAttribute(this.DATA_TREE_PATH)!;
        this.parsedTreePath = parseTreePath(this.treePath);

        this.nodeElement = this.element.querySelector('.node') as HTMLDivElement;
        this.argsElement = this.element.querySelector('.args') as HTMLDivElement | null;

        this.isPhantom = this.getParent()?.isPhantom || this.element.classList.contains(this.PHANTOM_CLASS) || false;

        const literalInputs = Array.from(this.nodeElement.querySelectorAll("input.literal[data-tree-path]:not([disabled])"))
            .map(input => createLiteralInput(input as HTMLInputElement));
        const exprSelectors = Array.from(
            this.nodeElement.querySelectorAll("select.expr-dropdown[data-tree-path]:not([disabled]), select.type-dropdown[data-tree-path]:not([disabled])"))
            .map(select => createExprSelector(select as HTMLSelectElement));
        this.inputs = (literalInputs as AbstractTreeInput[]).concat(exprSelectors);

        this.children = [];
        if (this.argsElement && this.argsElement.children) {
            Array.from(this.argsElement!.children!).forEach(element => {
                if (element.classList.contains('subtree')) {
                    const subNodePath = element.getAttribute(this.DATA_TREE_PATH)!;
                    const subNodePathHead = subNodePath.split('-')[0];
                    const subNodeString = getNodeStringFromPath(subNodePathHead, this.nodeString);
                    this.children.push(new Subtree(element as HTMLDivElement, this, subNodeString));
                }
            });
        }

        this.allInputs = this.inputs.concat(this.children.flatMap(child => child.getAllInputs()));

        let ruleAnnotationElement;
        if (this.argsElement) {
            ruleAnnotationElement = this.argsElement.lastElementChild;
        } else {
            ruleAnnotationElement = this.element.lastElementChild;
        }
        this.ruleAnnotation = new RuleAnnotation(ruleAnnotationElement as HTMLDivElement, this);

        this.doSetup();
    }

    private doSetup(): void {
        this.addHoverListeners();
        this.addClickListeners();
        if (this.isPhantom) {
            this.disableInputs();
        }
        replaceDisabledSelectInputs(this.nodeElement);
    }

    private addHoverListeners(): void {
        this.element.addEventListener('mouseover', (event) => {
            event.stopPropagation();
            if (!getContextMenuSelectedElement()) {
                this.getChildren().forEach(s => s.removeHighlight());
                this.addHighlight();
            }
        });
        this.element.addEventListener('mouseout', (event) => {
            event.stopPropagation();
            if (!getContextMenuSelectedElement()) {
                this.removeHighlight();
            }
        });
    }

    private addClickListeners(): void {
        const input = this.nodeElement.querySelector('input:not([disabled])');
        if (input instanceof HTMLInputElement) {
            this.element.addEventListener('click', event => {
                if (!this.hasHighlight()) return;
                event.preventDefault();
                input.focus();
                input.select();
            });
            this.element.querySelectorAll('input').forEach(input => {
                input.addEventListener('click', event => {
                    event.stopPropagation();
                });
            })
        }
    }

    private disableInputs(): void {
        this.inputs.forEach(input => input.disable());
    }

    addHighlight(): void {
        this.element.classList.add(this.HIGHLIGHT_CLASS);
    }

    removeHighlight(): void {
        this.element.classList.remove(this.HIGHLIGHT_CLASS);
    }

    hasHighlight(): boolean {
        return this.element.classList.contains(this.HIGHLIGHT_CLASS);
    }

    getElement(): HTMLDivElement {
        return this.element;
    }

    getNodeString(): string {
        return this.nodeString;
    }

    getTreePath(): number[] {
        return this.parsedTreePath;
    }

    getTreePathString(): string {
        return this.treePath;
    }

    getParent(): Subtree | null {
        return this.parent;
    }

    getChildren(): Subtree[] {
        return this.children;
    }

    getChildFromPath(treePath: number[]): Subtree | null {
        if (treePath.length === 0) {
            return this;
        }
        const next = this.children.find(child => child.getTreePath()[this.getTreePath().length] === treePath[0]);

        return next?.getChildFromPath(treePath.slice(1)) ?? null;
    }

    /**
     * Get all inputs in this subtree and its children.
     */
    getAllInputs(): AbstractTreeInput[] {
        if (this.isPhantom) {
            return [];
        }
        return this.allInputs;
    }

    getRuleAnnotation(): RuleAnnotation {
        return this.ruleAnnotation;
    }

    copy(keepParent: boolean = false): Subtree {
        const newElement = this.element.cloneNode(true) as HTMLDivElement;
        const clone = new Subtree(newElement, keepParent ? this.parent : null, this.nodeString);
        clone.removeHighlight();
        return clone;
    }
}

export function existingSubtreeFromElement(element: HTMLDivElement): Subtree | null {
    if (element.classList.contains('subtree')) {
        const treePathString = getTreePathOfElement(element);
        return getRootSubtree()?.getChildFromPath(parseTreePath(treePathString))! ?? null;
    }
    return null;
}
