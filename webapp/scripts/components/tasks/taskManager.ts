import {checkTaskFulfilled, getTasksList} from "../../serverRequest";
import {markHasCompletedFirstLangTasks} from "../../attention";
import {getLangSelector} from "../../globals/elements";
import {Task} from "./task";
import {ClassDict} from "../../globals/classDict";
import {IdDict} from "../../globals/idDict";

export function updateTaskList(lang: string, nodeString: string): void {
    getTaskManager().updateTaskList(lang, nodeString);
}

let taskManager: TaskManager | undefined = undefined;

function getTaskManager(): TaskManager {
    if (taskManager === undefined) {
        taskManager = new TaskManager();
    }
    return taskManager;
}

function isFulfilled(taskDiv: HTMLDivElement): boolean {
    return taskDiv.classList.contains(ClassDict.FULFILLED);
}

class TaskManager {
    private currentTasks: Task[] = [];
    private fulfilledTasks: string[] = [];
    private lastLang: string | undefined = undefined;

    private tasksDiv: HTMLDivElement;

    constructor() {
        this.tasksDiv = document.getElementById(IdDict.TASKS) as HTMLDivElement;
        if (!this.tasksDiv) {
            throw new Error("No tasks div found");
        }
    }

    public updateTaskList(lang: string, nodeString: string): void {
        const tasks: Task[] = getTasksList(lang);
        let allFulfilled = true;
        this.currentTasks = [];
        for (const task of tasks) {
            this.currentTasks.push(task);
            const {name} = task;

            if (this.fulfilledTasks.includes(name)) continue;

            const fulfilled = checkTaskFulfilled(lang, name, nodeString);
            if (fulfilled) {
                this.fulfilledTasks.push(name);
            } else {
                allFulfilled = false;
            }
        }

        if (this.lastLang !== lang) {
            const replacement = this.newTasksDiv();
            this.tasksDiv.replaceWith(replacement);
            this.tasksDiv = replacement;
        } else {
            this.updateTasksDiv();
        }
        this.lastLang = lang;

        if (getLangSelector().selectedIndex === 0 && allFulfilled) {
            markHasCompletedFirstLangTasks();
        }

        if (allFulfilled) {
            this.tasksDiv.classList.add(ClassDict.FULFILLED);
        } else {
            this.tasksDiv.classList.remove(ClassDict.FULFILLED);
        }
    }

    createTaskElement({name, description, difficulty}: Task): HTMLDivElement {
        const taskDiv = document.createElement("div");
        taskDiv.classList.add(ClassDict.TASK);
        taskDiv.setAttribute("data-task-name", name);

        const stars = "★".repeat(difficulty);

        const taskName = document.createElement("h3");
        taskName.textContent = `${stars} ${name}`;
        taskDiv.appendChild(taskName);

        const taskDescription = document.createElement("p");
        taskDescription.classList.add(ClassDict.DESCRIPTION);
        taskDescription.textContent = description;
        taskDiv.appendChild(taskDescription);

        if (this.fulfilledTasks.includes(name)) {
            taskDiv.classList.add(ClassDict.FULFILLED);
            taskDescription.style.maxHeight = "0";
        }

        taskDiv.addEventListener("mouseenter", () => {
            if (taskDiv.classList.contains(ClassDict.FULFILLED)) {
                taskDescription.style.maxHeight = taskDescription.scrollHeight + "px";
            }
        });
        taskDiv.addEventListener("mouseleave", () => {
            if (taskDiv.classList.contains(ClassDict.FULFILLED)) {
                taskDescription.style.maxHeight = "0";
            }
        });

        return taskDiv;
    }

    newTasksDiv(): HTMLDivElement {
        const newTasksDiv = document.createElement("div");
        newTasksDiv.id = this.tasksDiv.id;
        for (const cls of this.tasksDiv.classList) {
            newTasksDiv.classList.add(cls);
        }

        if (this.currentTasks.length === 0) {
            newTasksDiv.classList.add(ClassDict.HIDDEN);
        } else {
            newTasksDiv.classList.remove(ClassDict.HIDDEN);
            const taskElements = this.currentTasks.map(task => this.createTaskElement(task));
            newTasksDiv.append(...taskElements);
        }

        return newTasksDiv;
    }

    updateTasksDiv(): void {
        for (const task of this.currentTasks) {
            const taskDiv = this.tasksDiv.querySelector(`[data-task-name="${task.name}"]`);
            if (taskDiv && this.fulfilledTasks.includes(task.name) && taskDiv instanceof HTMLDivElement && !isFulfilled(taskDiv)) {
                taskDiv.classList.add(ClassDict.FULFILLED);
                const taskDescription = taskDiv.querySelector(`.${ClassDict.DESCRIPTION}`);
                if (taskDescription instanceof HTMLElement) {
                    taskDescription.style.maxHeight = taskDescription.scrollHeight + "px";
                    setTimeout(() => taskDescription.style.maxHeight = "0", 0);
                }
            }
        }
    }
}
