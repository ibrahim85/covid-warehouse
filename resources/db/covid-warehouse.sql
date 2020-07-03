-- :name drop-covid-day!
-- :command :execute
-- :result :raw
-- :doc drop the covid_day staging table
drop table covid_day if exists

-- :name create-covid-day!
-- :command :execute
-- :result :raw
-- :doc create the covid_day staging table
create table covid_day (
  date date,
  country varchar,
  state varchar,
  county varchar,
  case_total int,
  case_change int,
  death_total int,
  death_change int,
  recovery_total int,
  recovery_change int,
  primary key (date, country, state, county)
)

-- :name drop-dim-location!
-- :command :execute
-- :result :raw
-- :doc drop the dim_location table
drop table dim_location if exists

-- :name create-dim-location!
-- :command :execute
-- :result :raw
-- :doc create the covid_day staging table
create table dim_location (
  location_key uuid primary key,
  country varchar,
  state varchar,
  county varchar,
  unique (country, state, county))
