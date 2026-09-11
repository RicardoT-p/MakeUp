package com.makeup.mapper;

import com.makeup.model.Blogger;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BloggerMapper {

    String COLUMNS = "id, display_name, platform, platform_url, avatar_url, style_tags, enabled, "
            + "face_length_width, jaw_cheek_width, upper_third_cheek_width, lower_third_ratio, "
            + "eye_spacing_face_width, eye_aspect_ratio, nose_width_ratio, eyebrow_width_ratio, "
            + "eyebrow_thickness_ratio, mouth_width_ratio";

    @Select("SELECT " + COLUMNS + " FROM beauty_blogger WHERE enabled = TRUE ORDER BY id")
    @Results(id = "bloggerResult", value = {
            @Result(column = "display_name", property = "displayName"),
            @Result(column = "platform_url", property = "platformUrl"),
            @Result(column = "avatar_url", property = "avatarUrl"),
            @Result(column = "style_tags", property = "styleTags"),
            @Result(column = "face_length_width", property = "faceLengthWidth"),
            @Result(column = "jaw_cheek_width", property = "jawCheekWidth"),
            @Result(column = "upper_third_cheek_width", property = "upperThirdCheekWidth"),
            @Result(column = "lower_third_ratio", property = "lowerThirdRatio"),
            @Result(column = "eye_spacing_face_width", property = "eyeSpacingFaceWidth"),
            @Result(column = "eye_aspect_ratio", property = "eyeAspectRatio"),
            @Result(column = "nose_width_ratio", property = "noseWidthRatio"),
            @Result(column = "eyebrow_width_ratio", property = "eyebrowWidthRatio"),
            @Result(column = "eyebrow_thickness_ratio", property = "eyebrowThicknessRatio"),
            @Result(column = "mouth_width_ratio", property = "mouthWidthRatio")
    })
    List<Blogger> findAllEnabled();

    @Select("SELECT " + COLUMNS + " FROM beauty_blogger WHERE id = #{id}")
    @org.apache.ibatis.annotations.ResultMap("bloggerResult")
    Optional<Blogger> findById(long id);

    @Select("SELECT " + COLUMNS + " FROM beauty_blogger WHERE platform_url = #{platformUrl} LIMIT 1")
    @org.apache.ibatis.annotations.ResultMap("bloggerResult")
    Optional<Blogger> findByPlatformUrl(String platformUrl);

    @Insert("""
            INSERT INTO beauty_blogger
            (display_name, platform, platform_url, avatar_url, style_tags, enabled,
             face_length_width, jaw_cheek_width, upper_third_cheek_width, lower_third_ratio,
             eye_spacing_face_width, eye_aspect_ratio, nose_width_ratio, eyebrow_width_ratio,
             eyebrow_thickness_ratio, mouth_width_ratio)
            VALUES
            (#{displayName}, #{platform}, #{platformUrl}, #{avatarUrl}, #{styleTags}, #{enabled},
             #{metrics.faceLengthWidth}, #{metrics.jawCheekWidth}, #{metrics.upperThirdCheekWidth},
             #{metrics.lowerThirdRatio}, #{metrics.eyeSpacingFaceWidth}, #{metrics.eyeAspectRatio},
             #{metrics.noseWidthRatio}, #{metrics.eyebrowWidthRatio}, #{metrics.eyebrowThicknessRatio},
             #{metrics.mouthWidthRatio})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Blogger blogger);

    @Update("""
            UPDATE beauty_blogger SET display_name=#{displayName}, platform=#{platform},
            platform_url=#{platformUrl}, avatar_url=#{avatarUrl}, style_tags=#{styleTags}, enabled=#{enabled},
            face_length_width=#{metrics.faceLengthWidth}, jaw_cheek_width=#{metrics.jawCheekWidth},
            upper_third_cheek_width=#{metrics.upperThirdCheekWidth}, lower_third_ratio=#{metrics.lowerThirdRatio},
            eye_spacing_face_width=#{metrics.eyeSpacingFaceWidth}, eye_aspect_ratio=#{metrics.eyeAspectRatio},
            nose_width_ratio=#{metrics.noseWidthRatio}, eyebrow_width_ratio=#{metrics.eyebrowWidthRatio},
            eyebrow_thickness_ratio=#{metrics.eyebrowThicknessRatio}, mouth_width_ratio=#{metrics.mouthWidthRatio}
            WHERE id=#{id}
            """)
    int update(Blogger blogger);

    @Delete("DELETE FROM beauty_blogger WHERE id = #{id}")
    int delete(long id);
}
