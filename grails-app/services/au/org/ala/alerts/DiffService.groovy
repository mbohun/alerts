package au.org.ala.alerts

import com.jayway.jsonpath.JsonPath

import java.util.zip.GZIPInputStream

class DiffService {

  static transactional = true

  def serviceMethod() {}

  Boolean hasChangedJsonDiff(QueryResult queryResult){
    if(queryResult.lastResult != null && queryResult.previousResult != null){
      String last = decompressZipped(queryResult.lastResult)
      String previous = decompressZipped(queryResult.previousResult)
      hasChangedJsonDiff(previous, last, queryResult.query)
    } else {
      false
    }
  }

  Boolean hasChangedJsonDiff(QueryResult queryResult, String last, Query query){
    if(last != null && queryResult.previousResult != null){
      List<String> ids1 = JsonPath.read(last, query.recordJsonPath + "." + query.idJsonPath)
      String previous = decompressZipped(queryResult.previousResult)
      List<String> ids2 = JsonPath.read(previous, query.recordJsonPath + "." + query.idJsonPath)
      List<String> diff = ids1.findAll { !ids2.contains(it) }
      !diff.empty
    } else {
      false
    }
  }

  boolean isCollectionOrArray(object) {
        [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
  }

  Boolean hasChangedJsonDiff(String previous, String last, Query query){
    if(last != null && previous != null){

      def ids1 = JsonPath.read(last, query.recordJsonPath + "." + query.idJsonPath)
      if(!isCollectionOrArray(ids1)){
          ids1 = [ids1]
      }

      def ids2 = JsonPath.read(previous, query.recordJsonPath + "." + query.idJsonPath)
      if(!isCollectionOrArray(ids2)){
          ids2 = [ids2]
      }
      List<String> diff = ids1.findAll { !ids2.contains(it) }
      !diff.empty
    } else {
      false
    }
  }

  def getNewRecords(QueryResult queryResult){

    //decompress both and compare lists
    if(queryResult.query.recordJsonPath){
      String last = decompressZipped(queryResult.lastResult)
      JsonPath.read(last, queryResult.query.recordJsonPath)
    } else {
      []
    }
  }

  def getNewRecordsFromDiff(QueryResult queryResult){

    def records = []

    if(queryResult.lastResult != null && queryResult.previousResult != null){
      //decompress both and compare lists
      String last = decompressZipped(queryResult.lastResult)
      String previous = decompressZipped(queryResult.previousResult)

      List<String> ids1 = JsonPath.read(last, queryResult.query.recordJsonPath + "." +queryResult.query.idJsonPath)
      List<String> ids2 = JsonPath.read(previous, queryResult.query.recordJsonPath + "." +queryResult.query.idJsonPath)
      List<String> diff = ids1.findAll { !ids2.contains(it) }
      //pull together the records that have been added

      def allRecords = JsonPath.read(last, queryResult.query.recordJsonPath)
      allRecords.each { record ->
        if(diff.contains(record.get(queryResult.query.idJsonPath))){
          records.add(record)
        }
      }
    }
    records
  }

  def String decompressZipped(byte[] zipped){
    if(zipped){
        GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(zipped))
        StringBuffer sb = new StringBuffer()
        List<String> readed = null

        try {
          while (input.available() && !(readed = input.readLines()).isEmpty()) {
            sb.append(readed.join(""))
          }
        } catch (Exception e) {
          log.error(e.getMessage(), e)
        }
        input.close()
        sb.toString()
    } else {
        null
    }
  }
}
