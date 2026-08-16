package vn.coreplatform.kernel;

/**
 * Một module tích hợp vào kernel bằng cách expose đúng một descriptor.
 * Spring discovery tự thu thập mọi ModuleContributor bean; module tương lai chỉ cần
 * thêm contributor mới mà không sửa kernel.
 */
public interface ModuleContributor {
  ModuleDescriptor descriptor();
}
