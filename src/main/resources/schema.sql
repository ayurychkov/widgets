create sequence hibernate_sequence;

create table widget (
    id serial not null,
    x int not null,
    y int not null,
    z int not null,
    width int not null,
    height int not null,
    last_modification_date timestamp default now(),
    constraint widget_pk primary key (id)
);

create unique index widget_z_ix on widget (z);

