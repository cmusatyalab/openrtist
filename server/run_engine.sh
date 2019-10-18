#!/bin/bash

if ["$1" = ""]; then
    python3 main.py;
else
    python3 main.py -t $1 -o $2;
fi

