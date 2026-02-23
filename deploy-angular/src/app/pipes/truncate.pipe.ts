import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'truncate', standalone: true })
export class TruncatePipe implements PipeTransform {
  transform(value: string, max = 11): string {
    return value.length > max ? value.slice(0, max) + '…' : value;
  }
}
