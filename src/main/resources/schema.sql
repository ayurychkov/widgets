/*create schema lab;

create table lab.widget (
    id int auto_increment not null,
    x int not null,
    y int not null,
    z int not null,
    width int not null,
    height int not null,
    last_modification_date timestamp default now(),
    constraint widget_pk primary key (id)
);

create unique index lab.widget_z_ix on lab.widget (z);

*/