package com.agentdesk.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 手写 SQL：统计知识库总字数（CHAR_LENGTH 按字符而非字节计算，适配中文）。
     */
    @Select("SELECT COALESCE(SUM(CHAR_LENGTH(content)), 0) FROM document_chunks WHERE user_id = #{userId}")
    long sumContentLength(@Param("userId") Long userId);
}
