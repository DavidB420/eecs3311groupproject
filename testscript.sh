#!/bin/bash

#Example command: ./testscript.sh GET hasRelationship '{"actorId": "12345", "movieId": "1234"}'

curl -X $1 http://localhost:8080/api/v1/$2/ --data "$3" #'{ "actorId": "12345", "movieId": "1234" }'
