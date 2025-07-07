package com.lzb.gateway.dto.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzb.gateway.dto.entity.ServerInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerInfoMapper extends BaseMapper<ServerInfo> {
}
