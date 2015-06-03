Elasticsearch URL Token Filter
==============================

This plugin enables URL token filtering by URL part.

[![Build Status](https://secure.travis-ci.org/jlinn/elasticsearch-analysis-url.png?branch=master)](http://travis-ci.org/jlinn/elasticsearch-analysis-url)

## Compatibility

| Elasticsearch Version | Plugin Version |
|-----------------------|----------------|
| 1.5.2 | 1.1.0 |
| 1.4.2 | 1.0.0 |

## Installation
```bash
bin/plugin --install analysis-url --url https://github.com/jlinn/elasticsearch-analysis-url/releases/download/v1.0.0/elasticsearch-analysis-url-1.0.0.zip
```

## Usage
Options:
* `part`: This option defaults to `whole`, which will cause the entire URL to be returned. In this case, the filter only serves to validate incoming URLs. Other possible values are:
`protocol`, `host`, `port`, `path`, `query`, and `ref`.
* `url_decode`: Defaults to `false`. If `true`, the desired portion of the URL will be url decoded.

Set up your index like so:
```json
{
    "settings": {
        "analysis": {
            "filter": {
                "url_host": {
                    "type": "url",
                    "part": "host",
                    "url_decode": true
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

Make an anlysis request:
```bash
curl 'http://localhost:9200/index_name/_analyze?analyzer=url_host&pretty' -d 'https://foo.bar.com/baz.html'

{
  "tokens" : [ {
    "token" : "foo.bar.com",
    "start_offset" : 0,
    "end_offset" : 32,
    "type" : "word",
    "position" : 1
  } ]
}
```
