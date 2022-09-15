# User on account registration activity...

```
POST /user/create HTTP/1.1
Host: example.com
Content-Type: application/json
{
	"username": "example",
	"password": "P4ssw0rd!"
}
```

```
HTTP/1.1 200 OK
Server: Apache
Content-Type: application/json
{
	"identifier": 1
}
```

# Account registered, automatically login...

```
GET /user/session HTTP/1.1
Host: example.com
Content-Type: application/json
{
	"username": "example",
	"password": "P4ssw0rd!",
	"twofactor": null
	"duration": 2592000
}
```

```
HTTP/1.1 200 OK
Server: Apache
Content-Type: application/json
{
	"identifier": 1,
	"token": "e0c9035898dd52fc65c41454cec9c4d2611bfb37",
	"expires": 1662387266
}
```

# Logged in, user now on setup 2fa activity...

```
GET /user/twofactor HTTP/1.1
Host: example.com
Authorization: Token e0c9035898dd52fc65c41454cec9c4d2611bfb37
Content-Type: application/json
```

```
HTTP/1.1 200 OK
Server: Apache
Content-Type: application/json
{
	"algorithm": 1,
	"interval": 30,
	"length": 6,
	"secret": "2346ad27d7568ba9896f1b7da6b"
}
```

# Got 2fa details, user inputs 2fa code...

```
POST /user/twofactor HTTP/1.1
Host: example.com
Authorization: Token e0c9035898dd52fc65c41454cec9c4d2611bfb37
Content-Type: application/json
{
	"secret": "2346ad27d7568ba9896f1b7da6b",
	"code": "039103"
}
```

```
HTTP/1.1 204 No Content
Server: Apache
Content-Type: application/json
```

# 2fa now setup, 
