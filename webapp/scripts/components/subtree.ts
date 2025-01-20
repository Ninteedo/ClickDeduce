import {parseTreePath} from "../utils";
import {getContextMenuSelectedElement} from "./contextMenu";
import {createLiteralInput} from "./literalInput";
import {createExprSelector} from "./customExprSelector";
import {AbstractTreeInput} from "./abstractTreeInput";

export class Subtree {
    private readonly element: HTMLDivElement;
    private readonly parent: Subtree | null;
    private readonly children: Subtree[];

    private readonly treePath: string;
    private readonly nodeElement: HTMLDivElement;
    private readonly argsElement: HTMLDivElement | null;

    private readonly inputs: AbstractTreeInput[];

    private readonly isPhantom: boolean;

    private readonly HIGHLIGHT_CLASS = 'highlight';
    private readonly PHANTOM_CLASS = 'phantom';
    private readonly DATA_TREE_PATH = 'data-tree-path';

    constructor(element: HTMLDivElement, parent: Subtree | null) {
        this.element = element;
        this.parent = parent;

        this.treePath = this.element.getAttribute(this.DATA_TREE_PATH)!;
        this.nodeElement = this.element.querySelector('.node') as HTMLDivElement;
        this.argsElement = this.element.querySelector('.args') as HTMLDivElement | null;

        this.isPhantom = this.element.classList.contains(this.PHANTOM_CLASS) || this.getParent()?.isPhantom || false;

        const literalInputs = Array.from(this.nodeElement.querySelectorAll("input.literal[data-tree-path]:not([disabled])"))
            .map(input => createLiteralInput(input as HTMLInputElement));
        const exprSelectors = Array.from(
            this.nodeElement.querySelectorAll("select.expr-dropdown[data-tree-path]:not([disabled]), select.type-dropdown[data-tree-path]:not([disabled])"))
            .map(select => createExprSelector(select as HTMLSelectElement));
        this.inputs = (literalInputs as AbstractTreeInput[]).concat(exprSelectors);

        const myTreePath = parseTreePath(this.treePath);
        this.children = [];
        this.argsElement?.querySelectorAll('.subtree').forEach(element => {
            const treePathString = element.getAttribute(this.DATA_TREE_PATH);
            if (treePathString) {
                const treePath = parseTreePath(treePathString);
                if (treePath.length === myTreePath.length + 1) {
                    if (treePath[myTreePath.length] !== this.children.length) {
                        throw new Error("Subtrees not in correct order");
                    }

                    this.children.push(new Subtree(element as HTMLDivElement, this));
                }
            }
        });

        this.doSetup();
    }

    private doSetup(): void {
        this.addHoverListeners();
        this.addClickListeners();
        if (this.isPhantom) {
            this.disableInputs();
        }
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
        if (treePath[0] < 0 || treePath[0] >= this.children.length) {
            return null;
        }
        return this.children[treePath[0]].getChildFromPath(treePath.slice(1));
    }
}
