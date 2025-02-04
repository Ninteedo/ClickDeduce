import {checkTaskFulfilled, getTasksList} from "./serverRequest";
import {markHasCompletedFirstLangTasks} from "./attention";
import {getLangSelector} from "./globals/elements";

const FULFILLED_CLASS = "fulfilled";
const TASK_CLASS = "task";
const DESCRIPTION_CLASS = "description";
const HIDDEN_CLASS = "hidden";

export interface Task {
    name: string;
    description: string;
    difficulty: number;
}

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
    return taskDiv.classList.contains(FULFILLED_CLASS);
}

class TaskManager {
    private currentTasks: Task[] = [];
    private fulfilledTasks: string[] = [];
    private lastLang: string | undefined = undefined;

    private readonly tasksDiv: HTMLDivElement;

    constructor() {
        this.tasksDiv = document.getElementById("tasks") as HTMLDivElement;
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
            this.newTasksDiv();
        } else {
            this.updateTasksDiv();
        }
        this.lastLang = lang;

        if (getLangSelector().selectedIndex === 0 && allFulfilled) {
            markHasCompletedFirstLangTasks();
        }

        if (allFulfilled) {
            this.tasksDiv.classList.add(FULFILLED_CLASS);
        } else {
            this.tasksDiv.classList.remove(FULFILLED_CLASS);
        }
    }

    createTaskElement({name, description, difficulty}: Task): HTMLDivElement {
        const taskDiv = document.createElement("div");
        taskDiv.classList.add(TASK_CLASS);
        taskDiv.setAttribute("data-task-name", name);

        const stars = "★".repeat(difficulty);

        const taskName = document.createElement("h3");
        taskName.textContent = `${stars} ${name}`;
        taskDiv.appendChild(taskName);

        const taskDescription = document.createElement("p");
        taskDescription.classList.add(DESCRIPTION_CLASS);
        taskDescription.textContent = description;
        taskDiv.appendChild(taskDescription);

        if (this.fulfilledTasks.includes(name)) {
            taskDiv.classList.add(FULFILLED_CLASS);
            taskDescription.style.maxHeight = "0";
        }

        taskDiv.addEventListener("mouseenter", () => {
            if (taskDiv.classList.contains(FULFILLED_CLASS)) {
                taskDescription.style.maxHeight = taskDescription.scrollHeight + "px";
            }
        });
        taskDiv.addEventListener("mouseleave", () => {
            if (taskDiv.classList.contains(FULFILLED_CLASS)) {
                taskDescription.style.maxHeight = "0";
            }
        });

        return taskDiv;
    }

    newTasksDiv(): void {
        this.tasksDiv.innerHTML = "";

        if (this.currentTasks.length === 0) {
            this.tasksDiv.classList.add(HIDDEN_CLASS);
        } else {
            this.tasksDiv.classList.remove(HIDDEN_CLASS);
            for (const task of this.currentTasks) {
                this.tasksDiv.appendChild(this.createTaskElement(task));
            }
        }
    }

    updateTasksDiv(): void {
        for (const task of this.currentTasks) {
            const taskDiv = this.tasksDiv.querySelector(`[data-task-name="${task.name}"]`);
            if (taskDiv && this.fulfilledTasks.includes(task.name) && taskDiv instanceof HTMLDivElement && !isFulfilled(taskDiv)) {
                taskDiv.classList.add(FULFILLED_CLASS);
                const taskDescription = taskDiv.querySelector(".description");
                if (taskDescription instanceof HTMLElement) {
                    taskDescription.style.maxHeight = taskDescription.scrollHeight + "px";
                    setTimeout(() => taskDescription.style.maxHeight = "0", 0);
                }
            }
        }
    }
}
