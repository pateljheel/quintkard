import {
  CARD_PRIORITIES,
  CARD_STATUSES,
  CARD_TYPES
} from "./card-metadata";

export type UserResponse = {
  userId: string;
  displayName: string;
  email: string;
  redactionEnabled: boolean;
};

export type MessageStatus = "PENDING" | "PROCESSING" | "FAILED" | "SUCCESS";

export type MessageListItem = {
  id: string;
  userId: string;
  sourceService: string;
  externalMessageId: string | null;
  messageType: string;
  status: MessageStatus;
  summary: string | null;
  ingestedAt: string;
  sourceCreatedAt: string | null;
};

export type MessageResponse = {
  id: string;
  userId: string;
  sourceService: string;
  externalMessageId: string | null;
  messageType: string;
  status: MessageStatus;
  summary: string | null;
  payload: string;
  metadata: Record<string, unknown> | null;
  details: Record<string, unknown> | null;
  ingestedAt: string;
  sourceCreatedAt: string | null;
};

export type MessageSliceResponse = {
  items: MessageListItem[];
  page: number;
  size: number;
  hasNext: boolean;
};

export type AgentConfigResponse = {
  id: string;
  userId: string;
  name: string;
  description: string;
  prompt: string;
  model: string;
  temperature: number;
};

export type AgentSummaryResponse = {
  id: string;
  userId: string;
  name: string;
  description: string;
  model: string;
  temperature: number;
};

export type AgentConfigRequest = {
  name: string;
  description: string;
  prompt: string;
  model: string;
  temperature: number;
};

export type AgentModelConfigResponse = {
  id: string;
  label: string;
  minTemperature: number;
  maxTemperature: number;
  defaultTemperature: number;
};

export type AgentConfigMetadataResponse = {
  models: AgentModelConfigResponse[];
  defaultAgentModelId: string;
  defaultRoutingModelId: string;
  defaultFilteringModelId: string;
};

export type OrchestratorConfigRequest = {
  filteringPrompt: string;
  filteringModel: string;
  routingPrompt: string;
  routingModel: string;
  activeAgentIds: string[];
};

export type OrchestratorConfigResponse = {
  id: string | null;
  userId: string;
  filteringPrompt: string;
  filteringModel: string;
  routingPrompt: string;
  routingModel: string;
  activeAgents: AgentConfigResponse[];
};

export type CardType = (typeof CARD_TYPES)[number];

export type CardStatus = (typeof CARD_STATUSES)[number];

export type CardPriority = (typeof CARD_PRIORITIES)[number];

export type CardRequest = {
  title: string;
  summary: string | null;
  content: string;
  cardType: CardType;
  status: CardStatus;
  priority: CardPriority;
  dueDate: string | null;
  sourceMessageId: string | null;
};

export type CardListItem = {
  id: string;
  userId: string;
  title: string;
  summary: string | null;
  cardType: CardType;
  status: CardStatus;
  priority: CardPriority;
  dueDate: string | null;
  sourceMessageId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CardResponse = {
  id: string;
  userId: string;
  title: string;
  summary: string | null;
  content: string;
  cardType: CardType;
  status: CardStatus;
  priority: CardPriority;
  dueDate: string | null;
  sourceMessageId: string | null;
  createdAt: string;
  updatedAt: string;
};

export type CardSliceResponse = {
  items: CardListItem[];
  page: number;
  size: number;
  hasNext: boolean;
};
