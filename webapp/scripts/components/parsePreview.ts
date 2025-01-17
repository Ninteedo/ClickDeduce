export class ParsePreview {
    private readonly previewDiv: HTMLDivElement;
    private readonly PARSE_PREVIEW_CLASS = 'parse-preview';
    private readonly VISIBLE_CLASS = 'show';

    constructor(container: HTMLDivElement) {
        this.previewDiv = document.createElement('div');
        this.previewDiv.classList.add(this.PARSE_PREVIEW_CLASS);
        container.appendChild(this.previewDiv);
    }

    show(html: string): void {
        this.previewDiv.innerHTML = html;
        this.previewDiv.classList.add(this.VISIBLE_CLASS);
    }

    hide(): void {
        this.previewDiv.classList.remove(this.VISIBLE_CLASS);
    }
}
