// @ts-ignore
import TrueSvg from "../../../images/true.svg";
// @ts-ignore
import FalseSvg from "../../../images/false.svg";
import {LiteralInput} from "./literalInput";

export class LiteralBoolInput extends LiteralInput {
    constructor(input: HTMLInputElement) {
        super(input);
        this.input.addEventListener('change', () => {
            this.updateImage();
            this.handleInputChanged();
        });
        this.updateImage();
    }

    public override getValue(): string {
        return this.input.checked ? 'true' : 'false';
    }

    private updateImage(): void {
        const svg = this.input.checked ? TrueSvg : FalseSvg;
        this.input.style.content = `url("${svg}")`;
    }

    public override submit(): void {
        this.input.checked = !this.input.checked;
        super.submit();
    }
}
