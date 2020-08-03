# --- !Ups

alter table sm_image_resize
    add file_id varchar NOT NULL;

alter table sm_image_resize
    add constraint sm_image_resize_pk
        primary key (file_id);

# --- !Downs

alter table sm_image_resize
    drop constraint sm_image_resize_pk;

alter table sm_image_resize
drop file_id ;

