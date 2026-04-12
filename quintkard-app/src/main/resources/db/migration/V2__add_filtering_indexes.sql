create index idx_cards_user_card_type_updated_at
    on cards (user_fk, card_type, updated_at desc);

create index idx_messages_user_source_service_ingested_at
    on messages (user_fk, source_service, ingested_at desc);

create index idx_messages_user_message_type_ingested_at
    on messages (user_fk, message_type, ingested_at desc);
