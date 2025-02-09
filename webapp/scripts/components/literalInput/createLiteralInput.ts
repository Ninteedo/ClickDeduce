import {LiteralIdentifierLookupInput} from "./literalIdentifierLookupInput";
import {LiteralIntInput} from "./literalIntInput";
import {LiteralBoolInput} from "./literalBoolInput";
import {LiteralInput} from "./literalInput";

export function createLiteralInput(input: HTMLInputElement): LiteralInput {
    if (input.classList.contains('identifier-lookup')) {
        return new LiteralIdentifierLookupInput(input);
    } else if (input.classList.contains('integer')) {
        return new LiteralIntInput(input);
    } else if (input.type === 'checkbox') {
        return new LiteralBoolInput(input);
    } else {
        return new LiteralInput(input);
    }
}
