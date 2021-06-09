#!/usr/bin/env bash
if [[ "$1" == "" || "$2" == ""  ]]; then
    echo "This script requires two arguments, project to build and the 'build task' to run"
    echo "Note: user a '.' for the first argument to build all project"
    echo "Usage: $0 [arguments] \n"
    echo -e "     OR: $0 . 'ant clean package'"
    exit 1
fi
wd="/app"
task="package"

if [[ "$1" != "." ]]; then
    wd="$wd/$1"
fi

if [[ "$2" != "" ]]; then
    task="$2"
fi
docker build . -t lightside/builder
docker run --rm --name lightside-project -v "$PWD":/app -w "$wd" lightside/builder /bin/bash -c "${task}"