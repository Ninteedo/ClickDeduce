export class ClickDeduceResponseError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ClickDeduceResponseError";
    Object.setPrototypeOf(this, ClickDeduceResponseError.prototype)
  }
}
