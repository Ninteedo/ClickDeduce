import {getSelectedMode, parseTreePath} from "../utils";
import {getContextMenuSelectedElement} from "./contextMenu/contextMenu";
import {createExprSelector, CustomExprSelector, replaceDisabledSelectInputs} from "./customExprSelector";
import {AbstractTreeInput} from "./abstractTreeInput";
import {RuleAnnotation} from "./ruleAnnotation";
import {copyTreeNode, deleteTreeNode, pasteTreeNode, runAction} from "../actions";
import {lockPanZoom, unlockPanZoom} from "./panzoom";
import {pauseFileDragAndDrop, resumeFileDragAndDrop} from "../saveLoad";
import {getTreePathOfElement} from "../globals/elements";
import {getRootSubtree} from "../treeManipulation";
import {postProcessActionNew} from "../serverRequest";
import {PhantomIndicator} from "./phantomIndicator";
import {createLiteralInput} from "./literalInput/createLiteralInput";
import {getCurrentLanguage} from "../langSelector";
import {DisplayMode} from "../globals/displayMode";
import {ClassDict} from "../globals/classDict";

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
    private readonly phantomIndicator: PhantomIndicator | undefined;

    private readonly DATA_TREE_PATH = 'data-tree-path';

    constructor(element: HTMLDivElement, parent: Subtree | null, nodeString: string) {
        this.element = element;
        this.parent = parent;

        this.nodeString = nodeString;

        this.treePath = this.element.getAttribute(this.DATA_TREE_PATH)!;
        this.parsedTreePath = parseTreePath(this.treePath);

        this.nodeElement = this.element.querySelector('.node') as HTMLDivElement;
        this.argsElement = this.element.querySelector('.args') as HTMLDivElement | null;

        this.isPhantom = this.getParent()?.isPhantom || this.element.classList.contains(ClassDict.PHANTOM) || false;
        if (this.isPhantom) {
            this.phantomIndicator = new PhantomIndicator();
            this.element.appendChild(this.phantomIndicator.element);
        }

        const literalInputs = Array.from(this.nodeElement.querySelectorAll(`input.${ClassDict.LITERAL}[data-tree-path]:not([disabled])`))
            .map(input => createLiteralInput(input as HTMLInputElement));
        const exprSelectors = Array.from(
            this.nodeElement.querySelectorAll(`select.${ClassDict.EXPR_DROPDOWN}[data-tree-path]:not([disabled]), select.${ClassDict.TYPE_DROPDOWN}[data-tree-path]:not([disabled])`))
            .map(select => createExprSelector(select as HTMLSelectElement));
        exprSelectors.forEach(selector => {
            this.replaceExprSelectorPlaceholders(selector);
        })
        this.inputs = (literalInputs as AbstractTreeInput[]).concat(exprSelectors);

        this.children = [];
        if (this.argsElement && this.argsElement.children) {
            Array.from(this.argsElement!.children!).forEach(element => {
                if (element.classList.contains(ClassDict.SUBTREE)) {
                    const subNodeString = element.getAttribute('data-node-string')!;
                    this.children.push(new Subtree(element as HTMLDivElement, this, subNodeString));
                }
            });
        }

        this.allInputs = this.inputs.concat(this.children.flatMap(child => child.getAllInputs()));

        let ruleAnnotationElement;
        if (this.argsElement) {
            ruleAnnotationElement = Array.from(this.argsElement.children).find(child => child.classList.contains(ClassDict.ANNOTATION_NEW));
        } else {
            ruleAnnotationElement = this.element.querySelector(`.${ClassDict.ANNOTATION_AXIOM}`);
        }
        this.ruleAnnotation = new RuleAnnotation(ruleAnnotationElement as HTMLDivElement, this);

        this.element.tabIndex = 0;

        this.doSetup();
    }

    private doSetup(): void {
        this.addHoverListeners();
        this.addClickListeners();
        if (this.isPhantom) {
            this.disableInputs();
        }
        this.setupDragAndDrop();
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
        const input = this.getPrimaryInput();
        if (input) {
            this.element.addEventListener('click', event => {
                if (!this.hasHighlight()) return;
                event.preventDefault();
                input.focus();
            });
            this.element.querySelectorAll('input').forEach(input => {
                input.addEventListener('click', event => {
                    event.stopPropagation();
                });
            })
        }
    }

    private replaceExprSelectorPlaceholders(selector: CustomExprSelector): void {
        replaceDisabledSelectInputs(this.nodeElement, selector);
        this.getParent()?.replaceExprSelectorPlaceholders(selector);
    }

    public getPrimaryInput(): AbstractTreeInput | null {
        return (this.inputs.length > 0 && this.inputs[0]) || null;
    }

    private disableInputs(): void {
        this.inputs.forEach(input => input.disable());
    }

    addHighlight(): void {
        this.element.classList.add(ClassDict.HIGHLIGHT);
        this.phantomIndicator?.show();
    }

    removeHighlight(): void {
        this.element.classList.remove(ClassDict.HIGHLIGHT);
        this.phantomIndicator?.hide();
    }

    hasHighlight(): boolean {
        return this.element.classList.contains(ClassDict.HIGHLIGHT);
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

    disableAllInputs(): void {
        this.allInputs.forEach(input => input.disable());
    }

    getRuleAnnotation(): RuleAnnotation {
        return this.ruleAnnotation;
    }

    copy(keepParent: boolean = false, mode: DisplayMode | undefined = undefined): Subtree {
        const selectedMode = mode ?? getSelectedMode();

        const [, newHtml] = postProcessActionNew(getCurrentLanguage(), selectedMode, "IdentityAction", this.nodeString, "", []);
        const newElement = document.createElement('div');
        newElement.innerHTML = newHtml;
        const clone = new Subtree(newElement.firstElementChild as HTMLDivElement, keepParent ? this.parent : null, this.nodeString);
        clone.removeHighlight();
        return clone;
    }

    private setupDragAndDrop(): void {
        // this.element.setAttribute('draggable', 'true');

        const DRAG_HIGHLIGHT_CLASS = 'drag-highlight';
        const addDragHighlight = () => this.element.classList.add(DRAG_HIGHLIGHT_CLASS);
        const removeDragHighlight = () => this.element.classList.remove(DRAG_HIGHLIGHT_CLASS);

        // const clickAndHoldTime = 500;
        // let mouseDownStartTime: number | null = null;
        // this.element.addEventListener('mousedown', event => {
        //     event.stopPropagation();
        //     mouseDownStartTime = new Date().getTime();
        //     setTimeout(() => {
        //         if (mouseDownStartTime && new Date().getTime() - mouseDownStartTime >= clickAndHoldTime) {
        //             console.log('dispatching dragstart');
        //             this.element.dispatchEvent(new DragEvent('dragstart', { bubbles: true, dataTransfer: new DataTransfer() }));
        //         }
        //     }, clickAndHoldTime);
        // })
        // this.element.addEventListener('mouseup', () => {
        //     mouseDownStartTime = null;
        // });


        this.element.addEventListener('dragstart', event => {
            event.stopPropagation();
            console.debug('dragstart', 'path', this.treePath);
            lockPanZoom();
            pauseFileDragAndDrop();
            event.dataTransfer!.setData('text/plain', this.treePath);
            event.dataTransfer!.effectAllowed = 'move';
        });
        this.element.addEventListener('dragend', () => {
            unlockPanZoom();
            resumeFileDragAndDrop();
        });
        this.element.addEventListener('drop', event => {
            event.preventDefault();
            event.stopPropagation();

            console.debug(event.dataTransfer);

            const sourceTreePath = event.dataTransfer!.getData('text/plain');
            if (sourceTreePath) {
                if (sourceTreePath === this.treePath) {
                    return;
                }
                console.debug('dropped', sourceTreePath, 'onto', this.treePath);
                runAction("MoveAction", this.treePath, [sourceTreePath]);
            } else {
                const nodeString = event.dataTransfer!.getData('plain/subtreeNodeString');
                if (nodeString) {
                    console.debug('dropped subtree', nodeString, 'onto', this.treePath);
                    runAction("PasteAction", this.treePath, [nodeString]);
                }
            }
        });
        this.element.addEventListener('dragover', event => {
            event.preventDefault();
            event.stopPropagation();
            addDragHighlight();
        });
        this.element.addEventListener('dragleave', event => {
            event.preventDefault();
            removeDragHighlight();
        });
    }

    deleteAction(): void {
        deleteTreeNode(this.treePath);
    }

    copyToClipboard(): void {
        copyTreeNode(this.treePath);
    }

    pasteAction(): void {
        pasteTreeNode(this.treePath);
    }
}

export function existingSubtreeFromElement(element: HTMLDivElement): Subtree | null {
    if (element.classList.contains(ClassDict.SUBTREE)) {
        const treePathString = getTreePathOfElement(element);
        return getRootSubtree()?.getChildFromPath(parseTreePath(treePathString))! ?? null;
    }
    return null;
}
