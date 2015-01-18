Elasticsearch URL Token Filter
==============================

This plugin enables URL token filtering by URL part.

[![Build Status](https://secure.travis-ci.org/jlinn/elasticsearch-analysis-url.png?branch=master)](http://travis-ci.org/jlinn/elasticsearch-analysis-url)

## Compatibility

| Elasticsearch Version | Plugin Version |
|-----------------------|----------------|
| 1.4.2 | 1.0.0 |

## Installation
```bash
bin/plugin --install analysis-url --url https://github.com/jlinn/elasticsearch-analysis-url/releases/download/v1.0.0/elasticsearch-analysis-url-1.0.0.zip
```

## Usage
This filter only has one option: `part`. This option defaults to `whole`, which will cause the entire URL to be returned. In this case, the filter only serves to validate incoming URLs. Other possible values are:
`protocol`, `host`, `port`, `path`, `query`, and `ref`.

Set up your index like so:
```json
{
    "settings": {
        "analysis": {
            "filter": {
                "url_host": {
                    "type": "url",
                    "part": "host"
                }
            },
            "analyzer": {
                "url_host": {
                    "filter": ["url_host"],
                    "tokenizer": "whitespace"
                }
            }
        }
    },
    "mappings": {
        "example_type": {
            "properties": {
                "url": {
                    "type": "multi_field",
                    "fields": {
                        "url": {"type": "string"},
                        "host": {"type": "string", "analyzer": "url_host"}
                    }
                }
            }
        }
    }
}
```