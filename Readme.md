# A simple tool for importing data to MySql database from sql scripts.

## Usage
```shell
./mill import <sql_scripts_direction>
```

If you dont specify the direction, the default direction is `./scripts`. Then it will try
to read all the sql scripts in the direction and import them to the database.

## TODO:
- [ ] Support more database types.