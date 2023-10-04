# OpenRTiST (python-client)

## Overview

This is the desktop demo python client for Openrtist.
The backend can be deployed with Sinfonia.

## Dependencies:

The project uses [Poetry](https://python-poetry.org/) to manage dependencies.
To install all dependencies, [install `poetry`](https://python-poetry.org/docs/#installation)
and run the following command:

```bash
poetry install
```

## Usage:
### Run using the Poetry created virtualenv:

Connect to an already deployed OpenRTiST backend

```bash
poetry run openrtist --connect host:port
```

Run using a on-demand (Sinfonia) deployed backend

```bash
poetry run openrtist
```
