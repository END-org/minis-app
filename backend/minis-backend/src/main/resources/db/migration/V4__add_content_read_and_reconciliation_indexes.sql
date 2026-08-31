create index idx_content_items_unfiltered_public_feed
  on content_items (published_at desc, id desc)
  where status = 'PUBLISHED';

create index idx_content_items_published_reconciliation
  on content_items (id)
  where status = 'PUBLISHED';
