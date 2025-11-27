package com.side.hhplusecommerce.item.controller;

import com.side.hhplusecommerce.item.controller.dto.ItemResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemStockResponse;
import com.side.hhplusecommerce.item.controller.dto.ItemsResponse;
import com.side.hhplusecommerce.item.controller.dto.PopularItemsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "상품 관리", description = "상품 관리 API")
public interface ItemControllerDocs {

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 커서 기반 무한 스크롤 방식으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ItemsResponse> getItems(
            @Parameter(description = "마지막으로 조회한 상품 ID (다음 페이지 조회 시 사용)")
            Long cursor,
            @Parameter(description = "조회할 상품 수 (기본값: 20)")
            Integer size
    );

    @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ItemResponse> getItem(
            @Parameter(description = "조회할 상품 ID", required = true)
            Long itemId,
            @RequestParam(defaultValue = "false") Boolean isPopular
    );

    @Operation(summary = "인기 상품 조회", description = "최근 3일간 판매량 기준 상위 상품을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<PopularItemsResponse> getPopularItems(
            @Parameter(description = "조회할 상품 수 (기본값: 5, 최대: 10)")
            Integer limit
    );

    @Operation(summary = "상품 재고 확인", description = "특정 상품의 실시간 재고를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<ItemStockResponse> getItemStock(
            @Parameter(description = "상품 ID", required = true)
            Long itemId
    );
}
