export const CARD_TYPES = ["TASK", "NOTE", "REMINDER", "DECISION", "FOLLOW_UP", "ALERT"] as const;

export const CARD_STATUSES = ["OPEN", "IN_PROGRESS", "DONE", "ARCHIVED"] as const;

export const CARD_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const;

export const DEFAULT_CARD_TYPE = "TASK" as const;

export const DEFAULT_CARD_STATUS = "OPEN" as const;

export const DEFAULT_CARD_PRIORITY = "MEDIUM" as const;
