import {BaseDropdownSelector, DropdownOption, NameDropdownOption} from "../baseDropdownSelector";
import {LiteralInput} from "./literalInput";

class IdentifierDropdownSelector extends BaseDropdownSelector {
    private readonly identInput: LiteralIdentifierLookupInput;

    constructor(
        container: HTMLDivElement,
        input: HTMLInputElement,
        dropdown: HTMLDivElement,
        options: DropdownOption[],
        identInput: LiteralIdentifierLookupInput
    ) {
        super(container, input, dropdown, options);
        this.identInput = identInput;
    }

    override enterPressed(): void {
        super.enterPressed();
        this.identInput.submit();
    }
}

export class LiteralIdentifierLookupInput extends LiteralInput {
    constructor(input: HTMLInputElement) {
        super(input);
        const container = input.parentElement as HTMLDivElement;

        new IdentifierDropdownSelector(
            container,
            container.querySelector('input')!,
            container.querySelector('div.dropdown')!,
            Array.from(container.querySelectorAll('div.dropdown li')).map(option => new NameDropdownOption(option as HTMLLIElement)),
            this
        );
    }
}
