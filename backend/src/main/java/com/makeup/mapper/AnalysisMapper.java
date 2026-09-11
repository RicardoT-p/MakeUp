package com.makeup.mapper;

import com.makeup.model.AnalysisRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface AnalysisMapper {

    @Insert("INSERT INTO face_analysis(metrics_json, quality_score, result_json) VALUES(#{metricsJson}, #{qualityScore}, #{resultJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalysisRecord record);

    @Select("SELECT id, metrics_json, quality_score, result_json, created_at FROM face_analysis WHERE id = #{id}")
    @Results({
            @Result(column = "metrics_json", property = "metricsJson"),
            @Result(column = "quality_score", property = "qualityScore"),
            @Result(column = "result_json", property = "resultJson"),
            @Result(column = "created_at", property = "createdAt")
    })
    Optional<AnalysisRecord> findById(long id);
}

