import {LiteralInput} from "./literalInput";

export class LiteralIntInput extends LiteralInput {
    protected override onInit() {
        this.input.type = 'text';
    }

    protected override onInput(): void {
        super.onInput();
        const original = this.getValue();
        const onlyDigits = original.replace(/[^0-9]/g, '');
        if (original.startsWith('-')) {
            this.setValue('-' + onlyDigits);
        } else {
            this.setValue(onlyDigits);
        }
    }
}
