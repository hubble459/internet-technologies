# Opzetten Verbinding
Bij het verbinden met de server op poort 1337 krijg je een `INFO Welcome` bericht terug.

Na verbinden moet je inloggen.

Anders krijg je de error:
```shell script
s: 403 Please login first
```

c = Client
s = Server
e = Server to everyone
o = Server to everyone but you (Others)
r = Server to everyone in the room

Status codes:
2xx = Positive
4xx = Error

# Login
Om in te loggen moet je een `CONN [username]` bericht sturen.

### Happy Flow
Ingelogd als Quentin; wordt in de andere flows gebruikt als de ingelogde user.
```shell script
c: CONN Quentin
s: 200 Logged in as Quentin
o: 204 Quentin joined the server
```

### Sad Flow
```shell script
c: CONN [name that is already in use]
s: 401 User with username 'Quentin' is already logged in
```

```shell script
c: CONN [name that does not match requirements]
s: 402 Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

# Bericht Versturen
Als je een bericht naar iedereen op de server wilt versturen dan gebruik je `BCST`.

### Happy Flow
```shell script
c: BCST Hello World
e: BCST Quentin Hello World
```

### Sad Flow
No message
```shell script
c: BCST
s: 409 Please give a message to send
```

# Make Room
Make a room with the `MAKE` command.

### Happy Flow
```shell script
c: MAKE my_first_room
e: 203 my_first_room
```

### Sad Flow
```shell script
c: MAKE [name that does not match requirements or no name]
s: 402 Room name should be between 3 and 14 characters and should match [a-zA-Z_0-9]
```

# List Rooms
### Happy Flow
```shell script
c: ROOMS [should be empty, but doesnt matter]
s: ROOMS owo;swag;shrek;my_first_room
```
When there are no rooms:
```shell script
c: ROOMS
s: ROOMS
```
### Sad Flows
There's nothing sad about rooms

# Join Room

### Happy Flow
```shell script
c: JOIN my_first_room
r: Quentin joined my_first_room
```

### Sad Flow
```shell script
c: JOIN room_bestaat_niet
s: 400 Room with name 'room_bestaat_niet' does not exist!
```

# Talk in Room
Om in een room te praten gebruik je het `TALK` commando.



### Happy Flow

```shell script

```


