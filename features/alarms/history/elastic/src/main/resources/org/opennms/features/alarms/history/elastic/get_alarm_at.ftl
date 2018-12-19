{
  "query": {
    "bool": {
      "filter": [
        {
          "terms": {
            <#if reductionKey?has_content>
              "reduction-key": ["${reductionKey?json_string}"]
            <#else> <#-- If a reduction key is not set, then assume there's an alarmId -->
              "id": [${alarmId?long?c}]
            </#if>
          }
        },
        {
          "range": {
            "@update-time": {
              "gte": ${fromMillis?long?c},
              "format": "epoch_millis"
            }
          }
        },
        {
          "range": {
            "@update-time": {
              "lte": ${toMillis?long?c},
              "format": "epoch_millis"
            }
          }
        }
      ]
    }
  },
  "sort" : [
    { "@update-time" : {"order" : "desc"}}
  ],
  "size": 1
}