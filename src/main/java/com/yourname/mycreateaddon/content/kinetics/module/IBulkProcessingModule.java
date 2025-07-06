package com.yourname.mycreateaddon.content.kinetics.module;


import com.yourname.mycreateaddon.content.kinetics.base.IResourceAccessor;

/**
 * 드릴의 내부 버퍼 전체를 대상으로 대량/일괄 처리를 수행하는 모듈을 위한 인터페이스입니다.
 * (예: 압축, 자동 조합)
 */
public interface IBulkProcessingModule {

    /**
     * 벌크 처리를 시도합니다. 이 메서드는 일반 처리 모듈이 모두 작동한 후에 호출됩니다.
     * @param coreResources 드릴 구조의 자원에 접근하기 위한 액세서
     * @return 작업이 성공하여 내용물에 변화가 있었으면 true, 아니면 false
     */
    boolean processBulk(IResourceAccessor coreResources);

}